// This library is distributed in  the hope that it will be useful but without
// any  warranty; without  even  the  implied  warranty of  merchantability or
// fitness for a particular purpose.
// The use and distribution terms for this software are covered by the Eclipse
// Public License 1.0  (http://opensource.org/licenses/eclipse-1.0.php)  which
// can be found in the file epl-v10.html at the root of this distribution.
// By using this software in any  fashion, you are agreeing to be bound by the
// terms of this license. You  must not remove this notice, or any other, from
// this software.
// Copyright (c) 2013-2015, Ken Leung. All rights reserved.

package com.zotohlab.wflow;

/**
 * @author kenl
 *
 */
public class Delay extends Activity {

  public static Delay apply(String name, long delay) {
    return new Delay(name, delay);
  }

  public static Delay apply(long delay) {
    return apply("", delay);
  }

  public Delay(String name, long delay) {
    super(name);
    _delayMillis = delay;
  }

  public Delay(long delay) {
    this("", delay);
  }

  public Delay(String name) {
    this(name,0L);
  }

  public Delay() {
    this("", 0L);
  }

  public FlowDot reifyDot(FlowDot cur) {
    return new DelayDot(cur,this);
  }

  public void realize(FlowDot fp) {
    DelayDot p= (DelayDot) fp;
    p.withDelay(_delayMillis);
  }

  public long delayMillis() {
    return _delayMillis;
  }

  private long _delayMillis;
}


/**
 *
 * @author kenl
 *
 */
class DelayDot extends FlowDot {

  public long delayMillis() { return _delayMillis; }
  public FlowDot eval(Job j) { return this; }

  public DelayDot(FlowDot c, Delay a) {
    super(c,a);
  }

  public FlowDot withDelay(long millis) {
    _delayMillis=millis;
    return this;
  }

  private long _delayMillis= 0L;
}





