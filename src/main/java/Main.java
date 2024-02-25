import io.vertx.core.*;

import server.ForAnimals;
import server.ForUsers.*;

public class Main extends Launcher {

    public static void main(String[] args){
        Vertx vertx = Vertx.vertx();

        vertx.deployVerticle(new SignUp());
        vertx.deployVerticle(new DeleteUserById());
//        vertx.deployVerticle(new GetUserByParameter());
//        vertx.deployVerticle(new UpdateUserById());
//        vertx.deployVerticle(new GetUserById());
//        vertx.deployVerticle(new ForAnimals());

    }
}