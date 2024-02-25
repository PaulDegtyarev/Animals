package server.ForUsers;

import hibernate.Users;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
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
import java.util.List;

import static java.lang.System.out;

public class GetUserByParameter extends AbstractVerticle {

    private static  final PgConnectOptions connectOptions = new PgConnectOptions()
            .setPort(5432)
            .setHost("5.35.94.60")
            .setDatabase("animal-chipization")
            .setUser("postgres")
            .setPassword("105xPyj");

    @Override
    public void start(Promise<Void> startPromise) {

        Router router = Router.router(vertx);

        router.get("/accounts/search").handler(this::getuserByParameter);

        vertx.createHttpServer()
                .requestHandler(router)
                .listen(8080, http -> {
                    if (http.succeeded()) {
                        startPromise.complete();
                        out.println("HTTP server GetUserByParameter started on port 8080");
                    } else {
                        startPromise.fail(http.cause());
                    }
                });
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

}
