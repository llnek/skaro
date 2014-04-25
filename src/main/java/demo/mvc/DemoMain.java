/*??
// This library is distributed in  the hope that it will be useful but without
// any  warranty; without  even  the  implied  warranty of  merchantability or
// fitness for a particular purpose.
// The use and distribution terms for this software are covered by the Eclipse
// Public License 1.0  (http://opensource.org/licenses/eclipse-1.0.php)  which
// can be found in the file epl-v10.html at the root of this distribution.
// By using this software in any  fashion, you are agreeing to be bound by the
// terms of this license. You  must not remove this notice, or any other, from
// this software.
// Copyright (c) 2013 Cherimoia, LLC. All rights reserved.
 ??*/


package demo.mvc;

import java.text.SimpleDateFormat;
import java.util.Date;
import com.zotohlabs.gallifrey.runtime.AppMain;
import com.zotohlabs.gallifrey.core.Container;
import com.zotohlabs.gallifrey.io.*;
import org.json.*;
import com.zotohlabs.frwk.server.Service;

import com.zotohlabs.wflow.core.Job;
import com.zotohlabs.wflow.*;

/**
 * @author kenl
 *
 */
public class DemoMain implements AppMain {
  public void contextualize(Container c) {
  }
  public void initialize() {
    System.out.println("Point your browser to http://localhost:8000/test/hello");
  }
  public void configure(JSONObject j) {
  }
  public void start() {
  }
  public void stop() {
  }
  public void dispose() {
  }
}

