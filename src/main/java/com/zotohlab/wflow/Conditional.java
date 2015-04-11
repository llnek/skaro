// This library is distributed in  the hope that it will be useful but without
// any  warranty; without  even  the  implied  warranty of  merchantability or
// fitness for a particular purpose.
// The use and distribution terms for this software are covered by the Eclipse
// Public License 1.0  (http://opensource.org/licenses/eclipse-1.0.php)  which
// can be found in the file epl-v10.html at the root of this distribution.
// By using this software in any  fashion, you are agreeing to be bound by the
// terms of this license. You  must not remove this notice, or any other, from
// this software.
// Copyright (c) 2013, Ken Leung. All rights reserved.

package com.zotohlab.wflow;

/**
 * @author kenl
 *
 */
abstract class Conditional extends Activity {

  protected Conditional(String name, BoolExpr expr) {
    super(name);
    _expr= expr;
  }

  protected Conditional(BoolExpr expr) {
    this("", expr);
  }

  public BoolExpr expr() { return _expr; }

  private BoolExpr _expr;
}


abstract class ConditionalNode extends FlowNode {

  protected ConditionalNode(FlowNode c, Conditional a) {
    super(c,a);
  }

  public FlowNode withTest(BoolExpr expr) {
    _expr=expr;
    return this;
  }

  protected boolean test(Job j) {
    return _expr.evaluate(j);
  }

  private BoolExpr _expr;
}


