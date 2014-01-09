/*
 * Copyright 2013 The FIX.io Project
 *
 * The FIX.io Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package fixio.netty.codec;

import fixio.fixprotocol.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FixMessageEncoderTest {

    private static FixMessageEncoder encoder;
    @Mock
    private ChannelHandlerContext ctx;
    @Mock
    private ByteBufAllocator byteBufAllocator;
    private SimpleFixMessage fixMessage;
    private ByteBuf out;
    private long timestamp = 123456789;

    @BeforeClass
    public static void beforeClass() {
        encoder = new FixMessageEncoder();
    }

    @Before
    public void setUp() throws Exception {
        when(ctx.alloc()).thenReturn(byteBufAllocator);
        when(byteBufAllocator.buffer()).thenReturn(Unpooled.buffer());


        SimpleFixMessage fixMessage = new SimpleFixMessage();

        FixMessageHeader header = fixMessage.getHeader();

        header.setBeginString(FixMessage.FIX_4_2);
        header.setMessageType(MessageTypes.HEARTBEAT);
        header.setSenderCompID("SenderCompID");
        header.setTargetCompID("TargetCompID");
        header.setMsgSeqNum(2);

        fixMessage.add(1001, "test2");
        fixMessage.add(1000, "test1");

        this.fixMessage = fixMessage;
        fixMessage.getHeader().setSendingTime(timestamp);

        out = Unpooled.buffer();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFailIfNoBeginStringCompID() throws Exception {
        fixMessage.getHeader().setBeginString(null);

        encoder.encode(ctx, fixMessage, out);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFailIfNoMsgType() throws Exception {
        fixMessage.getHeader().setMessageType(null);

        encoder.encode(ctx, fixMessage, out);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFailIfNoSenderCompID() throws Exception {
        fixMessage.getHeader().setSenderCompID(null);

        encoder.encode(ctx, fixMessage, out);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFailIfNoTargetCompID() throws Exception {
        fixMessage.getHeader().setTargetCompID(null);

        encoder.encode(ctx, fixMessage, out);
    }

    @Test
    public void testEncode() throws Exception {

        encoder.encode(ctx, fixMessage, out);

        verify(ctx).flush();

        String expectedString = "8=FIX.4.2\u00019=89\u000135=0\u000149=SenderCompID\u000156=TargetCompID\u000134=2\u000152=19700102-10:17:36.789\u00011001=test2\u00011000=test1\u000110=204\u0001";

        assertResult(expectedString);
    }

    @Test
    public void testEncodeWithGroup() throws Exception {

        Group group1 = fixMessage.newGroup(1002);
        group1.add(1003, "g1-1");
        group1.add(1004, "g1-2");

        Group group2 = fixMessage.newGroup(1002);
        group2.add(1003, "g2-1");
        group2.add(1004, "g2-2");

        encoder.encode(ctx, fixMessage, out);

        verify(ctx).flush();

        String expectedString = "8=FIX.4.2\u00019=136\u000135=0\u000149=SenderCompID\u000156=TargetCompID\u000134=2\u000152=19700102-10:17:36.789\u00011001=test2\u00011000=test1\u00011002=2\u00011003=g1-1\u00011004=g1-2\u00011003=g2-1\u00011004=g2-2\u000110=014\u0001";

        assertResult(expectedString);
    }

    private void assertResult(String expectedString) {
        final String string = new String(out.array(), out.arrayOffset(), out.readableBytes(), US_ASCII);
        assertEquals(expectedString.length(), string.length());

        assertEquals(expectedString, string);
    }
}