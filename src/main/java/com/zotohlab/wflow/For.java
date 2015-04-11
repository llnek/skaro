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
 * A For is treated sort of like a while with the test-condition being (i &lt; upperlimit).
 *
 * @author kenl
 *
 */
public class For extends While {

  public static For apply(CounterExpr loopCount, Activity body) {
    return new For(loopCount, body);
  }

  public For(CounterExpr loopCount, Activity body) {
    // put a dummy bool-expr, not used.
    super((j) -> { return false; }, body);
    _loopCntr = loopCount;
  }

  public FlowNode reifyNode(FlowNode cur) {
    return new ForNode(cur,this);
  }

  public void realize(FlowNode n) {
    ForNode p= (ForNode) n;
    super.realize(n);
    p.withTest( new LoopExpr(p, _loopCntr));
  }

  private CounterExpr _loopCntr;
}

/**
 * @author kenl
 *
 */
class LoopExpr implements BoolExpr {

  public LoopExpr(FlowNode pt, CounterExpr cnt) {
    _point = pt;
    _cnt= cnt;
  }

  private CounterExpr _cnt;
  private FlowNode _point;

  private boolean _started=false;
  private int _loop=0;

  public boolean evaluate(Job j) {
    try {
      if (!_started) {
        _loop=_cnt.getCount(j);
        _started=true;
      }
      _point.tlog().debug("LoopExpr: loop {}", _loop);
      return _loop > 0;
    } finally {
      _loop -= 1;
    }
  }

}

class ForNode extends WhileNode {

  public ForNode(FlowNode c, For a) {
    super(c,a);
  }

}



