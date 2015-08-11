/*! LICENSE
 *
 * Copyright (c) 2015, The Agile Factory SA and/or its affiliates. All rights
 * reserved.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package framework.services.ext;

import java.io.InputStream;
import java.net.URL;

import org.xeustechnologies.jcl.JarClassLoader;
import org.xeustechnologies.jcl.ProxyClassLoader;

/**
 * A proxy class loader which encapsulates the Play class loader to be used by
 * the {@link JarClassLoader}.<br/>
 * It is required to resolve the play classes.
 * 
 * @author Pierre-Yves Cloux
 */
class PlayProxyClassLoader extends ProxyClassLoader {
    private ClassLoader environmentClassLoader;

    public PlayProxyClassLoader(ClassLoader environmentClassLoader) {
        this.environmentClassLoader = environmentClassLoader;
    }

    @Override
    public URL findResource(String name) {
        return getEnvironmentClassLoader().getResource(name);
    }

    @Override
    public Class<?> loadClass(String name, boolean resolveIt) {
        try {
            return getEnvironmentClassLoader().loadClass(name);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    @Override
    public InputStream loadResource(String name) {
        return getEnvironmentClassLoader().getResourceAsStream(name);
    }

    private ClassLoader getEnvironmentClassLoader() {
        return environmentClassLoader;
    }
}
