package server;

import hibernate.Users;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import io.vertx.pgclient.PgConnectOptions;
import org.hibernate.Session;
import org.hibernate.query.Query;
import utils.HibernateSessionFactoryUtil;

import java.util.List;

import static java.lang.System.out;


public class SelectEmail extends AbstractVerticle {

    private static final PgConnectOptions connectOptions = new PgConnectOptions()
            .setPort(5432)
            .setHost("5.35.94.60")
            .setDatabase("postgres")
            .setUser("postgres")
            .setPassword("123");

    @Override
    public void start(Promise<Void> startPromise) {

        Router router = Router.router(vertx);

        router.post("/selectEmail").handler(this::select_email);

        vertx.createHttpServer()
                .requestHandler(router)
                .listen(8080, http -> {
                    if (http.succeeded()) {
                        startPromise.complete();
                        out.println("HTTP server started on port 8080");
                    } else {
                        startPromise.fail(http.cause());
                    }
                });
    }

    private void select_email(RoutingContext routingContext) {
        routingContext.request().body().onComplete(bufferAsyncResult -> {
            if (bufferAsyncResult.succeeded()) {
                JsonObject info = new JsonObject(bufferAsyncResult.result());

                String authorizationToken = routingContext.request().getHeader("token");

                Integer id = info.getInteger("id");
                Session session = HibernateSessionFactoryUtil
                        .getSessionFactory().openSession();

                try {
                    session.beginTransaction();

                    Query query = session.createQuery("from Users where id = :userId");
                    query.setParameter("userId", id);

                    List<Users> users = query.list();
                    Users user = users.get(0);
                    String storedToken = user.getUserToken();
                    String storedEmail = user.getEmail();

                    if (authorizationToken != null && !authorizationToken.isEmpty()) {
                        if (authorizationToken.equals(storedToken)) {
                            routingContext.response().end(storedEmail);
                        } else {
                            routingContext.response().setStatusCode(400).end("This is not your current token");
                        }
                    }else {
                        routingContext.response().setStatusCode(400).end("Specify the token");
                    }
                    session.getTransaction().commit();
                } catch (Exception e){
                    e.printStackTrace();
                }finally {
                    session.close();
                }
            }
        });
    }
}