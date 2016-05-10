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


package com.zotohlab.skaro.runtime;

/**
 * @author kenl
 */
public interface RouteInfo  {

  public Object resemble(String mtd, String path);
  public Object collect(Object matcher);
  public Object getTemplate();
  public Object getHandler();
  public Object getPath();
  public boolean isStatic();
  public boolean isSecure();
  public Object getVerbs();

}

