package com.graphqlguy.moviedb.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_users")
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false)
    private String username;
    @Column(unique = true, nullable = false)
    private String email;
    @Column(nullable = false)
    private String password;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;


    public static class AppUserBuilder {
        private Long id;
        private String username;
        private String email;
        private String password;
        private Role role;

        AppUserBuilder() {
        }

        /**
         * @return {@code this}.
         */
        public AppUser.AppUserBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public AppUser.AppUserBuilder username(final String username) {
            this.username = username;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public AppUser.AppUserBuilder email(final String email) {
            this.email = email;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public AppUser.AppUserBuilder password(final String password) {
            this.password = password;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public AppUser.AppUserBuilder role(final Role role) {
            this.role = role;
            return this;
        }

        public AppUser build() {
            return new AppUser(this.id, this.username, this.email, this.password, this.role);
        }

        @Override
        public String toString() {
            return "AppUser.AppUserBuilder(id=" + this.id + ", username=" + this.username + ", email=" + this.email + ", password=" + this.password + ", role=" + this.role + ")";
        }
    }

    public static AppUser.AppUserBuilder builder() {
        return new AppUser.AppUserBuilder();
    }

    public Long getId() {
        return this.id;
    }

    public String getUsername() {
        return this.username;
    }

    public String getEmail() {
        return this.email;
    }

    public String getPassword() {
        return this.password;
    }

    public Role getRole() {
        return this.role;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public void setRole(final Role role) {
        this.role = role;
    }

    public AppUser() {
    }

    public AppUser(final Long id, final String username, final String email, final String password, final Role role) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
    }
}
