package apps.template.user.service;


import apps.template.user.repository.SubscribersRepository;
import apps.template.user.repository.UserCredentialsRepository;
import apps.template.user.repository.UserInfoRepository;
import apps.template.user.repository.UserTokenRepository;
import apps.template.user.repository.entity.SubscribersEntity;
import apps.template.user.repository.entity.UserCredentialsEntity;
import apps.template.user.repository.entity.UserInfoEntity;
import apps.template.user.repository.entity.UserTokenEntity;
import apps.template.user.transfer.SignupRequest;
import apps.template.user.transfer.SubscribeRequest;
import apps.template.user.transfer.UserCredentials;
import apps.template.user.transfer.UserInfo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.util.*;
import java.util.stream.Collectors;

import static apps.template.user.util.Passwords.*;

@Service
@Validated
public class UserService {

    @Autowired
    UserInfoRepository userInfoRepository;

    @Autowired
    UserCredentialsRepository userCredentialsRepository;

    @Autowired
    UserTokenRepository userTokenRepository;

    @Autowired
    SubscribersRepository subscribersRepository;

    public String authenticate(@Valid @NotNull final UserCredentials passedCredentials) {
        final UserInfoEntity userInfoEntity = userInfoRepository.findByEmail(passedCredentials.getUserName());
        if (userInfoEntity == null)
            throw new IllegalArgumentException("Invalid user name, could not find user with such user name");

        final UserCredentialsEntity existingCredentials = userCredentialsRepository.findByUserId(userInfoEntity.getId());
        if (existingCredentials == null)
            throw new IllegalStateException("No credentials are stored for the user, please signup again");

        if (!isExpectedPassword(passedCredentials.getPassword(),
                existingCredentials.getSalt(),
                existingCredentials.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid username/password");
        }

        final UserTokenEntity userToken = userTokenRepository.save(
                new UserTokenEntity(userInfoEntity.getId(), UUID.randomUUID().toString()));
        return userToken.getUserToken();
    }

    public String signup(@Valid @NotNull final SignupRequest signupRequest) {
        if (userInfoRepository.findByEmail(signupRequest.getEmail()) != null)
            throw new IllegalArgumentException("User with same email already exists");

        final UserInfoEntity user = userInfoRepository.save(new UserInfoEntity(signupRequest.getName(),
                signupRequest.getEmail(), signupRequest.getAvatarUrl()));

        final byte[] salt = getNextSalt();
        final byte[] hash = hash(signupRequest.getPassword(), salt);

        userCredentialsRepository.save(
                new UserCredentialsEntity(user.getId(),
                        user.getEmail(),
                        salt,
                        hash));

        final UserTokenEntity userToken = userTokenRepository.save(new UserTokenEntity(user.getId(),
                UUID.randomUUID().toString()));
        return userToken.getUserToken();
    }

    public boolean validateUserToken(final String userToken) {
        if (!StringUtils.hasLength(userToken))
            return false;

        return (userTokenRepository.findByUserToken(userToken) != null);
    }

    public UserInfo getUserForToken(final String userToken) {
        UserTokenEntity userTokenEntity = userTokenRepository.findByUserToken(userToken);
        if (userTokenEntity != null) {
            Optional<UserInfoEntity> userInfoEntityOptional = userInfoRepository.findById(userTokenEntity.getUserId());

            if (userInfoEntityOptional.isPresent()) {
                final UserInfoEntity userInfoEntity = userInfoEntityOptional.get();
                return new UserInfo(userInfoEntity.getId(), userInfoEntity.getEmail(),
                        userInfoEntity.getFullName(), userInfoEntity.getAvatarUrl());
            }
        }
        return null;
    }

    public List<UserInfo> getAllUsers() {
        List<UserInfoEntity> userInfoEntities = userInfoRepository.findAll();
        List<UserInfo> userInfos = new ArrayList<>();
        if (userInfoEntities != null) {
            for (final UserInfoEntity userInfoEntity : userInfoEntities) {
                userInfos.add(new UserInfo(userInfoEntity.getId(), userInfoEntity.getEmail(),
                        userInfoEntity.getFullName(), userInfoEntity.getAvatarUrl()));
            }
        }
        return userInfos;
    }

    public Set<String> getUserTokens(final Long userId) {
        final Set<UserTokenEntity> userTokens = userTokenRepository.findByUserId(userId);
        if (userTokens == null) {
            return Collections.emptySet();
        }
        return userTokens.stream()
                .map(UserTokenEntity::getUserToken)
                .collect(Collectors.toSet());
    }

    public boolean subscribe(@Valid @NotNull final SubscribeRequest subscribeRequest) {
        if (subscribersRepository.findByEmail(subscribeRequest.getEmail()) == null) {
            subscribersRepository.save(new SubscribersEntity(subscribeRequest.getEmail()));
        }
        return true;
    }
}
