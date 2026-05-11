package org.example.personalblogsystem.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.example.personalblogsystem.dto.LoginUserQueryRow;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LoginUser implements UserDetails, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final String REDIS_KEY_PREFIX = "login:user:";
    private static final String ROLE_PREFIX = "ROLE_";
    private static final String ENABLED_STATUS = "ENABLED";

    private Long id;
    private String userName;
    private String passwordHash;
    private String nickName;
    private String email;
    private String phone;
    private String avatarUrl;
    private String introduction;
    private Long roleId;
    private String roleCode;
    private String roleName;
    private String userStatus;
    private List<String> permissions;

    public LoginUser() {
    }

    public LoginUser(LoginUserQueryRow row) {
        this.id = row.getId();
        this.userName = row.getUserName();
        this.passwordHash = row.getPasswordHash();
        this.nickName = row.getNickName();
        this.email = row.getEmail();
        this.phone = row.getPhone();
        this.avatarUrl = row.getAvatarUrl();
        this.introduction = row.getIntroduction();
        this.roleId = row.getRoleId();
        this.roleCode = row.getRoleCode();
        this.roleName = row.getRoleName();
        this.userStatus = row.getUserStatus();
        this.permissions = buildPermissions(row.getRoleCode());
    }

    public static String redisKey(Long userId) {
        return REDIS_KEY_PREFIX + userId;
    }

    public String redisKey() {
        return redisKey(id);
    }

    public AdminAuthPrincipal toPrincipal() {
        return new AdminAuthPrincipal(id, userName, roleId, roleCode);
    }

    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return permissions == null
                ? List.of()
                : permissions.stream().map(SimpleGrantedAuthority::new).toList();
    }

    @Override
    @JsonIgnore
    public String getPassword() {
        return passwordHash;
    }

    @Override
    @JsonIgnore
    public String getUsername() {
        return userName;
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isEnabled() {
        return ENABLED_STATUS.equalsIgnoreCase(userStatus == null ? null : userStatus.trim());
    }

    private static List<String> buildPermissions(String roleCode) {
        if (!StringUtils.hasText(roleCode)) {
            return List.of();
        }
        return List.of(ROLE_PREFIX + roleCode.trim().toUpperCase());
    }

    public Long getId() {
        return id;
    }

    public String getUserName() {
        return userName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getNickName() {
        return nickName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getIntroduction() {
        return introduction;
    }

    public Long getRoleId() {
        return roleId;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public String getRoleName() {
        return roleName;
    }

    public String getUserStatus() {
        return userStatus;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public void setIntroduction(String introduction) {
        this.introduction = introduction;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
        this.permissions = buildPermissions(roleCode);
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public void setUserStatus(String userStatus) {
        this.userStatus = userStatus;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }
}
