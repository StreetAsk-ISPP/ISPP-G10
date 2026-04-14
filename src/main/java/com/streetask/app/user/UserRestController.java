/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.streetask.app.user;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.streetask.app.auth.AuthService;
import com.streetask.app.auth.payload.request.ChangeRoleRequest;
import com.streetask.app.auth.payload.response.JwtResponse;
import com.streetask.app.auth.payload.response.MessageResponse;
import com.streetask.app.exceptions.AccessDeniedException;
import com.streetask.app.payments.CheckoutReturnUrlRequestResolver;
import com.streetask.app.util.RestPreconditions;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/users")
@SecurityRequirement(name = "bearerAuth")
class UserRestController {

    private final UserService userService;
    private final AuthoritiesService authService;
    private final AuthService authServiceImpl;
    private final CheckoutReturnUrlRequestResolver checkoutReturnUrlRequestResolver;

    @Autowired
    public UserRestController(UserService userService, AuthoritiesService authService, AuthService authServiceImpl,
            CheckoutReturnUrlRequestResolver checkoutReturnUrlRequestResolver) {
        this.userService = userService;
        this.authService = authService;
        this.authServiceImpl = authServiceImpl;
        this.checkoutReturnUrlRequestResolver = checkoutReturnUrlRequestResolver;
    }

    @GetMapping
    public ResponseEntity<List<User>> findAll(@RequestParam(required = false) String auth) {
        List<User> res;
        if (auth != null) {
            res = (List<User>) userService.findAllByAuthority(auth);
        } else
            res = (List<User>) userService.findAll();
        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    @GetMapping("authorities")
    public ResponseEntity<List<Authorities>> findAllAuths() {
        List<Authorities> res = (List<Authorities>) authService.findAll();
        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    @GetMapping(value = "{id}")
    public ResponseEntity<User> findById(@PathVariable("id") UUID id) {
        return new ResponseEntity<>(userService.findUser(id), HttpStatus.OK);
    }

    @GetMapping(value = "{id}/reputation")
    public ResponseEntity<Integer> findReputationById(@PathVariable("id") UUID id) {
        User currentUser = userService.findCurrentUser();
        boolean isAdmin = currentUser.hasAuthority("ADMIN");
        boolean isOwner = currentUser.getId().equals(id);

        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException("You can only view your own reputation.");
        }

        User user = isOwner ? currentUser : userService.findUser(id);
        return new ResponseEntity<>(user.getReputation(), HttpStatus.OK);
    }

    @GetMapping(value = "me/reputation")
    public ResponseEntity<Integer> findCurrentUserReputation() {
        User user = userService.findCurrentUser();
        return new ResponseEntity<>(user.getReputation(), HttpStatus.OK);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<User> create(@RequestBody @Valid User user) {
        User savedUser = userService.saveUser(user);
        return new ResponseEntity<>(savedUser, HttpStatus.CREATED);
    }

    @PutMapping(value = "{userId}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<User> update(@PathVariable("userId") UUID id, @RequestBody @Valid User user) {
        RestPreconditions.checkNotNull(userService.findUser(id), "User", "ID", id);

        User currentUser = userService.findCurrentUser();
        boolean isAdmin = currentUser.hasAuthority("ADMIN");
        boolean isOwner = currentUser.getId().equals(id);

        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException("You don't have permission to edit this profile.");
        }
        return new ResponseEntity<>(this.userService.updateUser(user, id), HttpStatus.OK);
    }

    @DeleteMapping(value = "{userId}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<MessageResponse> delete(@PathVariable("userId") UUID id) {
        RestPreconditions.checkNotNull(userService.findUser(id), "User", "ID", id);
        if (!userService.findCurrentUser().getId().equals(id)) {
            userService.deleteUser(id);
            return new ResponseEntity<>(new MessageResponse("User deleted!"), HttpStatus.OK);
        } else
            throw new AccessDeniedException("You can't delete yourself!");
    }

    @GetMapping(value = "/{id}/stats")
    public ResponseEntity<Map<String, Object>> getUserStats(@PathVariable("id") UUID id) {
        RestPreconditions.checkNotNull(userService.findUser(id), "User", "ID", id);

        // Este método ya incluye 'bio' y 'profilePictureUrl' en el Map gracias al
        // cambio en UserService
        Map<String, Object> stats = userService.getUserStats(id);
        return new ResponseEntity<>(stats, HttpStatus.OK);
    }

    @GetMapping(value = "/{id}/questions")
    public ResponseEntity<Iterable<com.streetask.app.model.Question>> getUserQuestions(@PathVariable("id") UUID id) {
        RestPreconditions.checkNotNull(userService.findUser(id), "User", "ID", id);
        return new ResponseEntity<>(userService.findQuestionsByUserId(id), HttpStatus.OK);
    }

    @GetMapping(value = "/{id}/answers")
    public ResponseEntity<Iterable<com.streetask.app.model.Answer>> getUserAnswers(@PathVariable("id") UUID id) {
        RestPreconditions.checkNotNull(userService.findUser(id), "User", "ID", id);
        return new ResponseEntity<>(userService.findAnswersByUserId(id), HttpStatus.OK);
    }

    @PostMapping(value = "/me/premium/activate")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<MessageResponse> activateCurrentRegularPremium() {
        userService.updateCurrentRegularPremiumAccess(true);
        return new ResponseEntity<>(new MessageResponse("Regular premium access activated."), HttpStatus.OK);
    }

    @PostMapping(value = "/me/premium/deactivate")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<MessageResponse> deactivateCurrentRegularPremium() {
        userService.updateCurrentRegularPremiumAccess(false);
        return new ResponseEntity<>(new MessageResponse("Regular premium access deactivated."), HttpStatus.OK);
    }

    @PostMapping(value = "/me/premium/stripe/checkout-session")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<StripeCheckoutSessionResponse> createCurrentRegularPremiumStripeCheckoutSession(
            HttpServletRequest httpRequest) {
        String requestedReturnUrl = checkoutReturnUrlRequestResolver.resolve(httpRequest);
        StripeCheckoutSessionResponse response = userService
                .createCurrentRegularPremiumStripeCheckoutSession(requestedReturnUrl);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping(value = "/me/premium/stripe/confirm-session")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<MessageResponse> confirmCurrentRegularPremiumStripeCheckoutSession(
            @Valid @RequestBody StripeCheckoutSessionConfirmRequest request) {
        userService.confirmCurrentRegularPremiumStripeCheckoutSession(request.getSessionId());
        return new ResponseEntity<>(new MessageResponse("Regular premium access activated."), HttpStatus.OK);
    }

    @PutMapping(value = "/{id}/role")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<JwtResponse> changeUserRole(@PathVariable("id") UUID userId,
            @RequestBody @Valid ChangeRoleRequest request) {
        RestPreconditions.checkNotNull(userService.findUser(userId), "User", "ID", userId);

        User currentUser = userService.findCurrentUser();
        boolean isAdmin = currentUser.hasAuthority("ADMIN");
        boolean isSelf = currentUser.getId().equals(userId);

        // Only admin or user can change their own role
        if (!isAdmin && !isSelf) {
            throw new AccessDeniedException("You don't have permission to change this user's role.");
        }

        try {
            JwtResponse jwtResponse = authServiceImpl.changeUserAccountType(userId, request.getNewAccountType(),
                    isAdmin ? currentUser.getId() : null, request.getReason(), null);
            return new ResponseEntity<>(jwtResponse, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (AccessDeniedException e) {
            throw e;
        }
    }

}
