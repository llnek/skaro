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


package demo.async;

import com.zotohlabs.wflow.AsyncCallback;


/**
 * @author kenl
 *
 */
public enum DemoAsyncWS {
;

  public static void doLongAsyncCall(final AsyncCallback cb) {
    Thread t= new Thread(new Runnable() {
        public void run() {
          try {
            Thread.sleep(10000);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          cb.onSuccess("hello world");
        }
    });
    t.setDaemon(true);
    t.start();
  }

}
