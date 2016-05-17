package octoteam.tahiti.shared.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.util.HashMap;
import java.util.HashSet;

public class ExtendedContext {

    private final static AttributeKey<HashSet<String>> ATTR_KEY_IN_GROUPS = AttributeKey.valueOf("__in_groups");

    private HashMap<String, ChannelGroup> groups = new HashMap<>();

    private final ChannelFutureListener remover = future -> {
        Channel channel = future.channel();
        HashSet<String> g = channel.attr(ATTR_KEY_IN_GROUPS).get();
        if (g != null) {
            for (String group : g) {
                leave(channel, group);
            }
        }
    };

    public void join(Channel channel, String group) {
        // TODO: lock groups
        if (!groups.containsKey(group)) {
            groups.put(group, new DefaultChannelGroup(GlobalEventExecutor.INSTANCE));
        }
        groups.get(group).add(channel);
        HashSet<String> g = channel.attr(ATTR_KEY_IN_GROUPS).get();
        if (g == null) {
            g = new HashSet<>();
            channel.attr(ATTR_KEY_IN_GROUPS).set(g);
        }
        g.add(group);
        channel.closeFuture().addListener(remover);
    }

    public void leave(Channel channel, String group) {
        // TODO: lock groups
        if (!groups.containsKey(group)) {
            return;
        }
        HashSet<String> g = channel.attr(ATTR_KEY_IN_GROUPS).get();
        if (g != null) {
            g.remove(group);
        }
        ChannelGroup cg = groups.get(group);
        cg.remove(channel);
        if (cg.size() == 0) {
            groups.remove(group);
        }
        channel.closeFuture().removeListener(remover);
    }

    public String[] getJoinedGroups(Channel channel) {
        HashSet<String> g = channel.attr(ATTR_KEY_IN_GROUPS).get();
        if (g == null) {
            return new String[0];
        } else {
            return g.toArray(new String[g.size()]);
        }
    }

    public ChannelGroup of(String group) {
        if (groups.containsKey(group)) {
            return groups.get(group);
        } else {
            return new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
        }
    }

}
