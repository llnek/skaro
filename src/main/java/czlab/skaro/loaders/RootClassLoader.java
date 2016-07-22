/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright (c) 2013-2016, Kenneth Leung. All rights reserved. */


package czlab.skaro.loaders;

import java.io.File;

/**
 * @author Kenneth Leung
 */
public class RootClassLoader extends AbstractClassLoader {

  private File _baseDir=null;

  /**
   */
  public RootClassLoader(ClassLoader par, File home) {
    super(par);
    configure(home);
  }

  /**
   */
  public RootClassLoader(ClassLoader par) {
    this(par, new File(System.getProperty("skaro.home",".")));
  }

  /**
   */
  public File baseDir() { return _baseDir; }

  /**
   */
  public void configure(File baseDir) {
    if (baseDir != null) {
      load( baseDir);
    }
  }

  /**
   */
  private void load(File baseDir) {

    File p= new File(baseDir, "patch");
    File b= new File(baseDir, "lib");

    if (!_loaded) {
      findUrls(p).findUrls(b);
    }

    _baseDir=baseDir;
    _loaded=true;
  }

}

