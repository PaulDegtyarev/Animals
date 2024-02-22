package server;

import hibernate.Users;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;

import io.vertx.ext.auth.PubSecKeyOptions;

import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.pgclient.PgConnectOptions;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
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

        router.post("/registration").handler(this::handleSignUp);
        router.get("/accounts/search").handler(this::getuserByParameter);
        router.get("/accounts/:accountId").handler(this::getUserById);
        router.put("/accounts/:accountId").handler(this::updateUserById);


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

    private void handleSignUp(RoutingContext routingContext) {
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


    private void getUserById(RoutingContext routingContext) {

                String accountIdParam = routingContext.pathParam("accountId");
                Integer userId = null;
                if (!"null".equals(accountIdParam)) {
                    userId = Integer.parseInt(accountIdParam);
                }

                String userToken = routingContext.request().getHeader("token");

                Session session = HibernateSessionFactoryUtil
                        .getSessionFactory()
                        .openSession();

                if (userId == null || userId <= 0) {
                    routingContext.response().setStatusCode(400).end("User id cannot be absent or less than or equal to 0!");
                } else {
                    try {
                        Query queryForAuthorization = session.createQuery("from Users where usertoken = :userToken");
                        queryForAuthorization.setParameter("userToken", userToken);

                        List<Users> usersForAuthorization = queryForAuthorization.list();

                        if (usersForAuthorization.isEmpty()){
                            routingContext.response().setStatusCode(401).end("You are not authorize");
                        } else{
                            Users user = usersForAuthorization.get(0);

                            String storedUserRole = user.getRole();
                            Integer storedId = user.getId();

                            if (!storedId.equals(userId) && (storedUserRole.equals("CHIPPER") || storedUserRole.equals("USER"))){

                                routingContext.response().setStatusCode(403).end("Insufficient rights or someone else's account or account does not exist");

                            } else if ((storedId.equals(userId)) && (storedUserRole.equals("CHIPPER") || storedUserRole.equals("USER"))){
                                Query queryForNotAdmin = session.createQuery("from Users where id = :idUser");
                                queryForNotAdmin.setParameter("idUser", userId);

                                List<Users> resultForNotAdmin = queryForNotAdmin.list();


                                if (resultForNotAdmin.isEmpty()){
                                    routingContext.response().setStatusCode(403).end();
                                } else {
                                    Users userNotAdmin = resultForNotAdmin.get(0);

                                    Integer idNotAdmin = userNotAdmin.getId();
                                    String firstnameNotAdmin = userNotAdmin.getFirstName();
                                    String lastnameNotAdmin = userNotAdmin.getLastName();
                                    String emailNotAdmin = userNotAdmin.getEmail();
                                    String roleNotAdmin = userNotAdmin.getRole();

                                    JsonObject userJson = new JsonObject();
                                    userJson.put("id", idNotAdmin);
                                    userJson.put("firstName", firstnameNotAdmin);
                                    userJson.put("lastName", lastnameNotAdmin);
                                    userJson.put("email", emailNotAdmin);
                                    userJson.put("role", roleNotAdmin);

                                    routingContext.response().setStatusCode(200).end(userJson.encode());
                                }
                            } else if (storedUserRole.equals("ADMIN")){
                                Query queryForAdmin = session.createQuery("from Users where id = :idUser");
                                queryForAdmin.setParameter("idUser", userId);

                                List<Users> usersForAdmin = queryForAdmin.list();

                                if (usersForAdmin.isEmpty()){
                                    routingContext.response().setStatusCode(404).end("Account does not exist");
                                } else {

                                    Users userForAdmin = usersForAdmin.get(0);

                                    Integer idForAdmin = userForAdmin.getId();
                                    String firstnameForAdmin = userForAdmin.getFirstName();
                                    String lastnameForAdmin = userForAdmin.getLastName();
                                    String emailForAdmin = userForAdmin.getEmail();
                                    String roleForAdmin = userForAdmin.getRole();

                                    JsonObject userJson = new JsonObject();
                                    userJson.put("id", idForAdmin);
                                    userJson.put("firstName", firstnameForAdmin);
                                    userJson.put("lastName", lastnameForAdmin);
                                    userJson.put("email", emailForAdmin);
                                    userJson.put("role", roleForAdmin);

                                    routingContext.response().setStatusCode(200).end(userJson.encode());
                                }
                            }
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
    }

    private void getuserByParameter(RoutingContext routingContext) {

        String firstName = routingContext.request().getParam("firstName");
        String lastName = routingContext.request().getParam("lastName");
        String email = routingContext.request().getParam("email");
        Integer from = 0;
        Integer size = 10;

        String userToken = routingContext.request().getHeader("token");

        String fromParam = routingContext.request().getParam("from");
        if (fromParam != null && !fromParam.isEmpty()) {
            from = Integer.parseInt(routingContext.request().getParam("from"));
        }

        String sizeParam = routingContext.request().getParam("size");
        if (sizeParam != null && !sizeParam.isEmpty()) {
            size = Integer.parseInt(routingContext.request().getParam("size"));
        }

        Session session = HibernateSessionFactoryUtil
                .getSessionFactory()
                .openSession();

        if (from < 0 || size <= 0) {
            routingContext.response().setStatusCode(400).end("The from parameter cannot be less than 0, the size parameter cannot be less than or equal to 0");
        } else {
            try {
                session.beginTransaction();

                Query query = session.createQuery("from Users where usertoken = :userToken");
                query.setParameter("userToken", userToken);

                List<Users> users = query.list();

                if (users.isEmpty()){
                    routingContext.response().setStatusCode(401).end("You are not authorized");
                } else {
                    Users user = users.get(0);

                    String storedUserRole = user.getRole();
                    if (storedUserRole.equals("ADMIN")){

                        List<Predicate> predicates = new ArrayList<>();

                        CriteriaBuilder builder = session.getCriteriaBuilder();
                        CriteriaQuery<Users> criteriaQuery = builder.createQuery(Users.class);
                        Root<Users> root = criteriaQuery.from(Users.class);
                        criteriaQuery.select(root);

                        if (firstName != null && !firstName.isEmpty()){
                            predicates.add(builder.like(builder.lower(root.get("firstname")), "%" + firstName.toLowerCase() + "%"));
                        } else {

                        }
                        if (lastName != null){
                            predicates.add(builder.like(builder.lower(root.get("lastname")), "%" + lastName.toLowerCase() + "%"));
                        }
                        if (email != null){
                            predicates.add(builder.like(builder.lower(root.get("email")), "%" + email.toLowerCase() + "%"));
                        }

                        if (!predicates.isEmpty()){
                            criteriaQuery.where(builder.and(predicates.toArray(new Predicate[0])));
                        }
                        criteriaQuery.orderBy(builder.asc(root.get("id")));

                        List<Users> searchedUsers = session.createQuery(criteriaQuery)
                                .setFirstResult(from)
                                .setMaxResults(size)
                                .getResultList();

                        JsonArray userArray = new JsonArray();

                        for (Users searchedusers : searchedUsers){
                            JsonObject userJson = new JsonObject();
                            userJson.put("id", searchedusers.getId());
                            userJson.put("firstName", searchedusers.getFirstName());
                            userJson.put("lastName", searchedusers.getLastName());
                            userJson.put("email", searchedusers.getEmail());
                            userJson.put("role", searchedusers.getRole());
                            userArray.add(userJson);
                        }

                        routingContext.response().end(userArray.encode());

                    } else {
                        routingContext.response().setStatusCode(403).end("You do not have rights to do this action");
                    }
                }
                session.getTransaction().commit();

            } catch (Exception e){
                e.printStackTrace();
            } finally {
                session.close();
            }
        }

    }

    private void updateUserById(RoutingContext routingContext) {
        routingContext.request().body().onComplete(bufferAsyncResult -> {

            if (bufferAsyncResult.succeeded()) {

                JsonObject info = new JsonObject(bufferAsyncResult.result());

                String newFirstName = info.getString("firstName");
                String newLastName = info.getString("lastName");
                String newEmail = info.getString("email");
                String newPassword = info.getString("password");
                String newRole = info.getString("role");

                String accountIdParam = routingContext.pathParam("accountId");
                Integer accountId = null;
                if (!"null".equals(accountIdParam)) {
                    accountId = Integer.parseInt(accountIdParam);
                }

                String token = routingContext.request().getHeader("token");

                String roles[] = {"ADMIN", "CHIPPER", "USER"};
                ArrayList<String> rolesList = new ArrayList<>(Arrays.asList(roles));

                Session session = HibernateSessionFactoryUtil
                        .getSessionFactory()
                        .openSession();

                if (accountId == (null) || accountId <= 0) {
                    routingContext.response().setStatusCode(400).end("accountId cannot be null or less than 0");
                } else {
                    if (newFirstName == (null) || newFirstName.trim().isEmpty() || newLastName == (null) || newLastName.trim().isEmpty() || newEmail == (null) || newEmail.trim().isEmpty() || !rolesList.contains(newRole) || newPassword.equals("null") || newPassword.trim().isEmpty()) {
                        routingContext.response().setStatusCode(400).end();
                    } else {
                        Query queryForUpdate = session.createQuery("FROM Users where id = :userId");
                        queryForUpdate.setParameter("userId", accountId);

                        List<Users> usersForUpdate = queryForUpdate.list();

                        Query query = session.createQuery("from Users where usertoken = :userToken");
                        query.setParameter("userToken", token);
                        List<Users> usersList = query.list();

                        if (usersList.isEmpty()){
                            routingContext.response().setStatusCode(401).end("You are not authorized");
                        } else {
                            Users user = usersList.get(0);
                            String storedUserRole = user.getRole();

                            if (storedUserRole.equals("CHIPPER") || storedUserRole.equals("USER")){
                                Integer storedId = user.getId();

                                out.println(storedUserRole);
                                out.println(storedId);
                                out.println(accountId);

                                if (!storedId.equals(accountId)){
                                    routingContext.response().setStatusCode(403).end("Updating an account that is not yours");
                                } else {
                                    session.beginTransaction();

                                    user.setFirstName(newFirstName);
                                    user.setLastName(newLastName);
                                    user.setEmail(newEmail);
                                    user.setPassword(newPassword);
                                    user.setRole(newRole);

                                    session.update(user);
                                    session.getTransaction().commit();

                                    routingContext.response().setStatusCode(200).end("Successfully updated!");

                                }
                            } else if (storedUserRole.equals("ADMIN")){
                                if (usersForUpdate.isEmpty()){
                                    routingContext.response().setStatusCode(404).end("Account not found");
                                } else {
                                    session.beginTransaction();
                                    try {
                                        Query queryToCheckEmail = session.createQuery("from Users where email = :userEmail");
                                        queryToCheckEmail.setParameter("userEmail", newEmail);

                                        List<Users> usersListToCheckEmail = queryToCheckEmail.list();
                                        if (!usersListToCheckEmail.isEmpty()){
                                            routingContext.response().setStatusCode(409).end("An account with the same email already exists");
                                        } else {
                                                Users userForUpdate = usersForUpdate.get(0);
                                                userForUpdate.setFirstName(newFirstName);
                                                userForUpdate.setLastName(newLastName);
                                                userForUpdate.setEmail(newEmail);
                                                userForUpdate.setPassword(newPassword);
                                                userForUpdate.setRole(newRole);

                                                session.update(userForUpdate);

                                                session.getTransaction().commit();

                                                routingContext.response().end("Successfully updated!");
                                            }
                                    } catch (Exception e){
                                        session.getTransaction().rollback();
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    }
                }
                session.close();
            }
        });
    }
}