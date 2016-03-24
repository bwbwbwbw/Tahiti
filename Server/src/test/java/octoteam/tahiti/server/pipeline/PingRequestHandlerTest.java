package octoteam.tahiti.server.pipeline;

import io.netty.channel.embedded.EmbeddedChannel;
import octoteam.tahiti.protocol.SocketMessageProtos.Message;
import octoteam.tahiti.protocol.SocketMessageProtos.PingPongBody;
import org.junit.Assert;
import org.junit.Test;

public class PingRequestHandlerTest {

    @Test
    public void testPingRequest() throws Exception {

        // PingRequestHandler should handle PING_REQUEST

        EmbeddedChannel channel = new EmbeddedChannel(new PingRequestHandler());

        Message pingRequest = Message.newBuilder()
                .setSeqId(123)
                .setDirection(Message.DirectionCode.REQUEST)
                .setService(Message.ServiceCode.PING_REQUEST)
                .setPingPong(PingPongBody.newBuilder()
                        .setPayload("magic payload")
                )
                .build();

        channel.writeInbound(pingRequest);
        channel.finish();

        // this message should be consumed
        // so that next handler will not receive it
        Assert.assertNull(channel.readInbound());

        // expect a response from PingRequestHandler
        Object response = channel.readOutbound();
        Assert.assertTrue(response instanceof Message);

        Message responseMsg = (Message) response;
        Assert.assertTrue(responseMsg.isInitialized());
        Assert.assertEquals(123, responseMsg.getSeqId());
        Assert.assertEquals(Message.DirectionCode.RESPONSE, responseMsg.getDirection());
        Assert.assertTrue(responseMsg.getPingPong().isInitialized());
        Assert.assertEquals("magic payload", responseMsg.getPingPong().getPayload());

    }

    @Test
    public void testOtherRequest() throws Exception {

        // PingRequestHandler should NOT handle messages other than PING_REQUEST

        EmbeddedChannel channel = new EmbeddedChannel(new PingRequestHandler());

        Message otherRequest = Message.newBuilder()
                .setSeqId(321)
                .setDirection(Message.DirectionCode.REQUEST)
                .setService(Message.ServiceCode.USER_SIGN_IN_REQUEST)
                .build();

        channel.writeInbound(otherRequest);
        channel.finish();

        // next handler should be able to read this "otherRequest"
        Assert.assertEquals(otherRequest, channel.readInbound());

        // no outbound data should be written
        Assert.assertNull(channel.readOutbound());

    }

}