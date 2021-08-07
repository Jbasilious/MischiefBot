import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

public class Main {

    private static final Map<String, Command> commands = new HashMap<>();
    static {
        commands.put("hello",event -> event.getMessage().getChannel()
                .flatMap(channel -> channel.createMessage("Hi, I'm Mischief Bot"))
                .then());
    }

    public static void main(String[] args) {
        GatewayDiscordClient client = DiscordClientBuilder.create(Config.BOT_TOKEN)
                .build()
                .login()
                .block();

        assert client != null;


        client.getEventDispatcher().on(ReadyEvent.class).subscribe(event -> {
            final User self = event.getSelf();
            System.out.println(String.format("Logged in as %s#%s", self.getUsername(), self.getDiscriminator()));
        });


        client.getEventDispatcher().on(MessageCreateEvent.class)
                .flatMap(message -> Mono.just(message.getMessage().getContent())
                .flatMap(content -> Flux.fromIterable(commands.entrySet())
                .filter(entry -> content.startsWith('!' + entry.getKey()))
                .flatMap(entry -> entry.getValue().execute(message))
                .next()))
                .subscribe();

        client.onDisconnect().block();
    }

}