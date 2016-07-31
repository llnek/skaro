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


package czlab.skaro.io;

import czlab.skaro.server.Replyable;
import java.net.HttpCookie;
import java.util.Set;
import czlab.xlib.XData;



/**
 * @author Kenneth Leung
 */
public interface HTTPEvent extends IOEvent, Replyable {

  /**/
  public HttpCookie cookie(String name);

  /**/
  public Iterable<HttpCookie> cookies();

  /**/
  public boolean isKeepAlive();

  /**/
  public XData data();

  /**/
  public boolean hasData();

  /**/
  public long contentLength();

  /**/
  public String contentType();

  /**/
  public String encoding();

  /**/
  public String contextPath();

  /**/
  public Iterable<String> headerValues(String nm);

  /**/
  public Set<String> headers();

  /**/
  public String header(String nm);

  /**/
  public boolean hasHeader(String nm);

  /**/
  public Iterable<String> paramValues(String nm);

  /**/
  public Set<String> params();

  /**/
  public String param(String nm);

  /**/
  public boolean hasParam(String nm);

  /**/
  public String localAddr();

  /**/
  public String localHost();

  /**/
  public int localPort();

  /**/
  public String method();

  /**/
  public String protocol();

  /**/
  public String host();

  /**/
  public String queryStr();

  /**/
  public String remoteAddr();

  /**/
  public String remoteHost();

  /**/
  public int remotePort();

  /**/
  public String scheme();

  /**/
  public String serverName();

  /**/
  public int serverPort();

  /**/
  public boolean isSSL();

  /**/
  public String uri();

  /**/
  public String requestURL();

}



