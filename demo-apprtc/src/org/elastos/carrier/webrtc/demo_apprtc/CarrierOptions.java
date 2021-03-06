/*
 * Copyright (c) 2018 Elastos Foundation
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.elastos.carrier.webrtc.demo_apprtc;


import org.elastos.carrier.Carrier;

import java.io.File;
import java.util.ArrayList;

class CarrierOptions extends Carrier.Options {
    CarrierOptions(String path) {
        super();

        File file = new File(path);
        if (file.exists())
            file.delete();
        file.mkdir();

        try {
            setUdpEnabled(true);
            setPersistentLocation(path);

            ArrayList<BootstrapNode> arrayList = new ArrayList<>();
            BootstrapNode node = new BootstrapNode();
            node.setIpv4("13.58.208.50");
            node.setPort("33445");
            node.setPublicKey("89vny8MrKdDKs7Uta9RdVmspPjnRMdwMmaiEW27pZ7gh");
            arrayList.add(node);

            node = new BootstrapNode();
            node.setIpv4("18.216.102.47");
            node.setPort("33445");
            node.setPublicKey("G5z8MqiNDFTadFUPfMdYsYtkUDbX5mNCMVHMZtsCnFeb");
            arrayList.add(node);

            node = new BootstrapNode();
            node.setIpv4("18.216.6.197");
            node.setPort("33445");
            node.setPublicKey("H8sqhRrQuJZ6iLtP2wanxt4LzdNrN2NNFnpPdq1uJ9n2");
            arrayList.add(node);

            node = new BootstrapNode();
            node.setIpv4("52.83.171.135");
            node.setPort("33445");
            node.setPublicKey("5tuHgK1Q4CYf4K5PutsEPK5E3Z7cbtEBdx7LwmdzqXHL");
            arrayList.add(node);

            node = new BootstrapNode();
            node.setIpv4("52.83.191.228");
            node.setPort("33445");
            node.setPublicKey("3khtxZo89SBScAMaHhTvD68pPHiKxgZT6hTCSZZVgNEm");
            arrayList.add(node);

            setBootstrapNodes(arrayList);

            ArrayList<ExpressNode> expressNodes = new ArrayList<>();
            ExpressNode enode = new ExpressNode();
            enode.setIpv4("ece00.trinity-tech.io");
            enode.setPort("443");
            enode.setPublicKey("FyTt6cgnoN1eAMfmTRJCaX2UoN6ojAgCimQEbv1bruy9");
            expressNodes.add(enode);

            enode = new ExpressNode();
            enode.setIpv4("ece01.trinity-tech.io");
            enode.setPort("443");
            enode.setPublicKey("FyTt6cgnoN1eAMfmTRJCaX2UoN6ojAgCimQEbv1bruy9");
            expressNodes.add(enode);

            enode = new ExpressNode();
            enode.setIpv4("ece01.trinity-tech.cn");
            enode.setPort("443");
            enode.setPublicKey("FyTt6cgnoN1eAMfmTRJCaX2UoN6ojAgCimQEbv1bruy9");
            expressNodes.add(enode);

            setExpressNodes(expressNodes);
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
