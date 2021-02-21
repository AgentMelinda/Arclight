package io.izzel.arclight.common.mixin.bukkit;

import com.google.common.io.ByteStreams;
import io.izzel.arclight.common.asm.SwitchTableFixer;
import io.izzel.arclight.common.bridge.bukkit.JavaPluginLoaderBridge;
import io.izzel.arclight.common.mod.util.remapper.ArclightRemapper;
import io.izzel.arclight.common.mod.util.remapper.ClassLoaderRemapper;
import io.izzel.arclight.common.mod.util.remapper.RemappingClassLoader;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.util.Map;
import java.util.jar.Manifest;

@Mixin(targets = "org.bukkit.plugin.java.PluginClassLoader", remap = false)
public class PluginClassLoaderMixin extends URLClassLoader implements RemappingClassLoader {

    // @formatter:off
    @Shadow @Final private Map<String, Class<?>> classes;
    @Shadow @Final private JavaPluginLoader loader;
    @Shadow @Final private PluginDescriptionFile description;
    @Shadow @Final private Manifest manifest;
    @Shadow @Final private URL url;
    // @formatter:on

    private ClassLoaderRemapper remapper;

    @Override
    public ClassLoaderRemapper getRemapper() {
        if (remapper == null) {
            remapper = ArclightRemapper.createClassLoaderRemapper(this);
        }
        return remapper;
    }

    public PluginClassLoaderMixin(URL[] urls) {
        super(urls);
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    Class<?> findClass(String name, boolean checkGlobal) throws ClassNotFoundException {
        if (name.startsWith("org.bukkit.") || name.startsWith("net.minecraft.")) {
            throw new ClassNotFoundException(name);
        }
        Class<?> result = classes.get(name);

        if (result == null) {
            if (checkGlobal) {
                result = ((JavaPluginLoaderBridge) (Object) loader).bridge$getClassByName(name);
            }

            if (result == null) {
                String path = name.replace('.', '/').concat(".class");
                URL url = this.findResource(path);

                if (url != null) {
                    byte[] classBytes;

                    URLConnection connection;
                    CodeSigner[] signers;
                    try {
                        connection = url.openConnection();
                        try (InputStream is = connection.getInputStream()) {
                            classBytes = ByteStreams.toByteArray(is);
                            if (connection instanceof JarURLConnection) {
                                signers = ((JarURLConnection) connection).getJarEntry().getCodeSigners();
                            } else {
                                signers = new CodeSigner[0];
                            }
                        } catch (IOException ex) {
                            throw new ClassNotFoundException(name, ex);
                        }
                    } catch (IOException e) {
                        throw new ClassNotFoundException(name, e);
                    }

                    classBytes = SwitchTableFixer.INSTANCE.processClass(classBytes);
                    classBytes = Bukkit.getUnsafe().processClass(description, path, classBytes);
                    classBytes = this.getRemapper().remapClass(classBytes);

                    int dot = name.lastIndexOf('.');
                    if (dot != -1) {
                        String pkgName = name.substring(0, dot);
                        if (getPackage(pkgName) == null) {
                            try {
                                if (manifest != null) {
                                    definePackage(pkgName, manifest, this.url);
                                } else {
                                    definePackage(pkgName, null, null, null, null, null, null, null);
                                }
                            } catch (IllegalArgumentException ex) {
                                if (getPackage(pkgName) == null) {
                                    throw new IllegalStateException("Cannot find package " + pkgName);
                                }
                            }
                        }
                    }

                    CodeSource source = new CodeSource(this.url, signers);

                    result = defineClass(name, classBytes, 0, classBytes.length, source);
                }

                if (result == null) {
                    result = super.findClass(name);
                }

                if (result != null) {
                    ((JavaPluginLoaderBridge) (Object) loader).bridge$setClass(name, result);
                }
            }

            classes.put(name, result);
        }

        return result;
    }
}
