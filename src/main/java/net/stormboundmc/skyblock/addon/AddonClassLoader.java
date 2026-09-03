package net.stormboundmc.skyblock.addon;

import java.net.URL;
import java.net.URLClassLoader;

final class AddonClassLoader extends URLClassLoader {

    AddonClassLoader(URL jarUrl, ClassLoader parent) {
        super(new URL[] { jarUrl }, parent);
    }
}
