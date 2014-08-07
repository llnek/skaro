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

package demo.fork;

import com.zotohlab.wflow.*;
import com.zotohlab.wflow.core.Job;

/**
 * @author kenl
 *
    parent(s1) --> split&nowait
                   |-------------> child(s1)----> split&wait --> grand-child
                   |                              |                    |
                   |                              |<-------------------+
                   |                              |---> child(s2) -------> end
                   |
                   |-------> parent(s2)----> end
 */
public class Demo implements PipelineDelegate {

    // split but no wait
    // parent continues;

  public Activity getStartActivity(Pipeline pipe) {
    return new PTask( new Work() {
      public Object perform(FlowNode cur, Job job, Object arg) {
        System.out.println("I am the *Parent*");
        System.out.println("I am programmed to fork off a parallel child process, and continue my business.");
        return null;
      }
    }).chain( new Split().addSplit(new PTask(new Work() {
      public Object perform(FlowNode cur, Job job, Object arg) {
        System.out.println("*Child*: will create my own child (blocking)");
        job.setv("rhs", 60);
        job.setv("lhs", 5);
        Activity p2= new PTask( new Work() {
          public Object perform(FlowNode cur, Job job, Object arg) {
            System.out.println("*Child*: the result for (5 * 60) according to my own child is = "  +
                        job.getv("result"));
            System.out.println("*Child*: done.");
            return null;
          }
        });
                  // split & wait
        return new Split( new And(p2)).addSplit(new PTask(new Work() {
          public Object perform(FlowNode cur, Job job, Object arg) {
            System.out.println("*Child->child*: taking some time to do this task... ( ~ 6secs)");
            for (int i= 1; i < 7; ++i) {
              try {
                Thread.sleep(1000);
              } catch (InterruptedException e) {
                e.printStackTrace();
              }
              System.out.print("...");
            }
            System.out.println("");
            System.out.println("*Child->child*: returning result back to *Child*.");
            job.setv("result",  (Integer) job.getv("rhs") * (Integer) job.getv("lhs"));
            System.out.println("*Child->child*: done.");
            return null;
          }
        }));
      }
    }) )).chain(new PTask( new Work() {
      public Object perform(FlowNode cur, Job job, Object arg) {
        System.out.println("*Parent*: after fork, continue to calculate fib(6)...");
        StringBuilder b=new StringBuilder("*Parent*: ");
        for (int i=1; i < 7; ++i) {
          b.append( fib(i) + " ");
        }
        System.out.println(b.toString()  + "\n" + "*Parent*: done.");
        return null;
      }
    }));
  }

  public void onStop(Pipeline p) {}
  public Activity onError(Throwable e, FlowNode p) { return null; }

  private int fib(int n) {
    return (n <3) ? 1 : fib(n-2) + fib(n-1);
  }

}

