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

package com.zotohlab.frwk.dbio;

import com.jolbox.bonecp.ConnectionHandle;
import com.jolbox.bonecp.hooks.AbstractConnectionHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author kenl
 */
public class BoneCPHook extends AbstractConnectionHook {

  private static final Logger _log= LoggerFactory.getLogger(BoneCPHook.class);
  public static Logger tlog() { return _log; }

  public void onCheckOut(ConnectionHandle h) {
    tlog().debug("BoneCP: checking out a connection =======================> {}", h);
  }

  public void onCheckIn(ConnectionHandle h) {
    tlog().debug("BoneCP: checking in a connection   =======================> {}", h);
  }

}