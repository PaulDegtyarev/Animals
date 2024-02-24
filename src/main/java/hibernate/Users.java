package hibernate;

import java.io.Serializable;

import jakarta.persistence.*;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import java.util.Base64;

@Entity
@Table(name = "users", schema = "animals")
public class Users implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @OneToMany(mappedBy = "users")
    private Integer id;

    @Column(name =  "firstname")
    public String firstname;

    @Column(name = "lastname")
    public String lastname;

    @Column(name = "email")
    public String email;

    @Column(name = "password")
    public String password;

    @Column(name = "role")
    public String role;

    @Column(name = "usertoken")
    public String usertoken;

    public Integer getId(){

        return id;

    }

    public void setFirstName(String firstname){

        this.firstname = firstname;

    }

    public String getFirstName(){

        return firstname;

    }

    public void setLastName(String lastName){

        this.lastname = lastName;

    }

    public String getLastName(){

        return lastname;

    }


    public void setEmail(String email){

        this.email = email;

    }

    public String getEmail(){

        return email;

    }

    public String getPassword (){

        return password;

    }

    public void setPassword(String password){
        try {
            String hashedPassword = generateHashedPassword(password);
            this.password = hashedPassword;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e){
            e.printStackTrace();
        }

    }

    private String generateHashedPassword(String password) throws NoSuchAlgorithmException, InvalidKeySpecException {

        SecureRandom random = new SecureRandom().getInstanceStrong();
        byte[] salt = new byte[16];
        random.nextBytes(salt);

        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 128);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] hash = factory.generateSecret(spec).getEncoded();

        return Base64.getEncoder().encodeToString(hash);
    }

    public void setUserToken(String usertoken){

        this.usertoken = usertoken;

    }

    public void setRole(String role){
        this.role = role;
    }

    public String getRole(){
        return role;
    }

    public String  getUserToken(){

        return usertoken;

    }


    @Override
    public String toString() {

        return this.getEmail() + this.getPassword();

    }


}