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

package com.zotohlab.frwk.netty;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInboundHandlerAdapter;


/**
 * Wrapper so that we can do some reference count debugging.
 *
 * @author kenl
 */
@ChannelHandler.Sharable
public abstract class InboundAdapter extends ChannelInboundHandlerAdapter {

  private static Logger _log = getLogger(lookup().lookupClass());
  public Logger tlog() { return _log; }

  protected InboundAdapter() {}

}
