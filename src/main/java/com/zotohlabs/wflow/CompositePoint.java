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

package com.zotohlabs.wflow;

import java.util.ListIterator;

/**
 * @author kenl
 *
 */
public abstract class CompositePoint extends FlowPoint {

  protected CompositePoint(FlowPoint cur,Activity a) {
    super(cur,a);
  }

  protected Iter _inner = null;

  public void reifyInner( ListIterator<Activity> children) {
    _inner=new Iter(this,children);
  }

  public Iter inner() { return _inner; }

}
