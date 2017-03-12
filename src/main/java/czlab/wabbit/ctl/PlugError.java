/**
 * Copyright (c) 2013-2017, Kenneth Leung. All rights reserved.
 * The use and distribution terms for this software are covered by the
 * Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 * which can be found in the file epl-v10.html at the root of this distribution.
 * By using this software in any fashion, you are agreeing to be bound by
 * the terms of this license.
 * You must not remove this notice, or any other, from this software.
 */

package czlab.wabbit.ctl;

/**
 * @author Kenneth Leung
 */
public class PlugError extends Exception {

  private static final long serialVersionUID = 1L;

  /**/
  public PlugError(String msg, Throwable e) {
    super(msg, e);
  }

  /**/
  public PlugError(Throwable e) {
    this(null,e);
  }

  /**/
  public PlugError(String msg) {
    this(msg,null);
  }

}

