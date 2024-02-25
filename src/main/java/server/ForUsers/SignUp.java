package server.ForUsers;

import hibernate.Users;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;

import io.vertx.ext.auth.PubSecKeyOptions;

import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.pgclient.PgConnectOptions;

import org.hibernate.Session;

import org.hibernate.query.Query;
import utils.HibernateSessionFactoryUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.System.out;


public class SignUp extends AbstractVerticle {
    private static  final  PgConnectOptions connectOptions = new PgConnectOptions()
            .setPort(5432)
            .setHost("5.35.94.60")
            .setDatabase("animal-chipization")
            .setUser("postgres")
            .setPassword("105xPyj");

    @Override
    public void start(Promise<Void> startPromise) {

        Router router = Router.router(vertx);

        router.post("/registration").handler(this::signUp);

        vertx.createHttpServer()
                .requestHandler(router)
                .listen(8080, http -> {
                    if (http.succeeded()) {
                        startPromise.complete();
                        out.println("HTTP server SignUp started on port 8080");
                    } else {
                        startPromise.fail(http.cause());
                    }
                });
    }

    public void signUp(RoutingContext routingContext) {
        routingContext.request().body().onComplete(bufferAsyncResult -> {

            if (bufferAsyncResult.succeeded()) {
                JsonObject info = new JsonObject(bufferAsyncResult.result());
                String firstname = info.getString("firstName");
                String lastname = info.getString("lastName");
                String email = info.getString("email");
                String password = info.getString("password");
                String confrimpassword = info.getString("confirmPassword");
                boolean agree = info.getBoolean("agreeToPolicy");

                String role = routingContext.request().getHeader("role");
                String userToken = routingContext.request().getHeader("token");

                String emailPattern = "^[a-z0-9]+@[a-z0-9]+(\\.[a-z]{2,})$";
                String roles[] = {"ADMIN", "CHIPPER", "USER"};
                ArrayList<String> rolesList = new ArrayList<>(Arrays.asList(roles));

                JWTAuthOptions config = new JWTAuthOptions()
                        .addPubSecKey(new PubSecKeyOptions()
                                .setAlgorithm("HS256")
                                .setBuffer("keyboard cat"));

                JWTAuth provider = JWTAuth.create(vertx, config);

                if (firstname == null || lastname == null || email == null || password == null || confrimpassword == null || !agree || role == null) {
                    routingContext.response().setStatusCode(400).end("Enter all details");
                } else if (firstname.trim().isEmpty()){
                    routingContext.response().setStatusCode(400).end("Write your firstname");
                } else if (lastname.trim().isEmpty()){
                    routingContext.response().setStatusCode(400).end("Write your lastname");
                } else if(!email.trim().matches(emailPattern)) {
                    routingContext.response().setStatusCode(400).end("Your email is not valid");
                } else if(!password.trim().matches(confrimpassword)) {
                    routingContext.response().setStatusCode(400).end("Password mismatch");
                } else if(!agree) {
                    routingContext.response().setStatusCode(400).end("You must agree to the terms and conditions and security policy");
                } else if (role.isEmpty()){
                    routingContext.response().setStatusCode(400).end("Write your role");
                } else if (!rolesList.contains(role)) {
                    routingContext.response().setStatusCode(400).end("Write your role correctly!");
                } else {
                    String token = provider.generateToken(new JsonObject().put("us",email), new JWTOptions());

                    Session session = HibernateSessionFactoryUtil
                            .getSessionFactory().openSession();
                    try {
                        session.beginTransaction();

                        Query query = session.createQuery("from Users where usertoken = :userToken");
                        query.setParameter("userToken", userToken);

                        List<Users> users = query.list();

                        if (!users.isEmpty()) {
                            Users user = users.get(0);
                            String storedEmail = user.getEmail();
                            String storedToken = user.getUserToken();

                            if (email.matches(storedEmail)) {
                                routingContext.response().setStatusCode(409).end("An account with the same email already exists");
                            } else if (storedToken.matches(userToken)) {
                                routingContext.response().setStatusCode(403).end("You are already logged in!");
                            } else {
                                user.setFirstName(firstname);
                                user.setLastName(lastname);
                                user.setEmail(email);
                                user.setPassword(password);
                                user.setRole(role);
                                user.setUserToken(token);
                                session.persist("users", user);
                                session.getTransaction().commit();

                                routingContext.response()
                                        .putHeader("content-type", "application/json")
                                        .end("You are sign up succsess");
                            }
                        } else {
                            Users user = new Users();

                            user.setFirstName(firstname);
                            user.setLastName(lastname);
                            user.setEmail(email);
                            user.setPassword(password);
                            user.setRole(role);
                            user.setUserToken(token);

                            session.persist("users", user);
                            session.getTransaction().commit();

                            routingContext.response()
                                    .putHeader("content-type", "application/json")
                                    .end("You are sign up succsess");
                        }

                    } catch (Exception e){
                        session.getTransaction().rollback();
                        e.printStackTrace();
                    } finally {
                        session.close();
                    }

                }
            }
        });
    }
}