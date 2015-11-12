package org.jetbrains.sbt;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * @author Pavel Fatin
 */
public class Launcher {
    private static final String SBT_LAUNCHER_MAIN_CLASS = "xsbt.boot.Boot";
    private static final String CONVERTER_CLASS_PREFIX = "org.jetbrains.";

    public static void main(String[] args) throws Exception {
        URL thisJar = Launcher.class.getProtectionDomain().getCodeSource().getLocation();
        String thisJarPath = new File(thisJar.toURI().getPath()).getPath(); // TODO spaces?

        if (!thisJarPath.endsWith(".jar")) {
            System.err.println("Launcher must be run as a jar file.");
            System.exit(-1);
        }

        ClassLoader parentClassLoader = ClassLoader.getSystemClassLoader().getParent();

        URLClassLoader classLoader = new URLClassLoader(new URL[]{thisJar}, parentClassLoader) {
            @Override
            protected Class findClass(String name) throws ClassNotFoundException {
                if (name.startsWith(CONVERTER_CLASS_PREFIX)) {
                    throw new ClassNotFoundException(name);
                }
                return super.findClass(name);
            }

            @Override
            protected Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.startsWith(CONVERTER_CLASS_PREFIX)) {
                    throw new ClassNotFoundException(name);
                }

                return super.loadClass(name, resolve);
            }
        };

        Class sbtLauncherClass = Class.forName(SBT_LAUNCHER_MAIN_CLASS, true, classLoader);
        Method method = sbtLauncherClass.getMethod("main", String[].class);

        String command = ";apply -cp " + thisJarPath + " org.jetbrains.sbt.ConverterPlugin ;intellij-convert";

        method.invoke(null, new Object[]{new String[]{command}});
    }
}
