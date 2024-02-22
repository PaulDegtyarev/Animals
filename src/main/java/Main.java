import io.vertx.core.*;


import server.SelectEmail;
import server.SignUp;

public class Main extends Launcher {

    public static void main(String[] args){
        Vertx vertx = Vertx.vertx();

        vertx.deployVerticle(new SignUp());
//        vertx.deployVerticle(new SelectEmail());

    }
}