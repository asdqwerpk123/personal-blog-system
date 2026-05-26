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

/**
 * Spring Security 登录用户模型，承载数据库用户、角色和权限信息。
 * 同时作为 Redis 登录态缓存对象，供 JWT 过滤器恢复 SecurityContext 使用。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoginUser implements UserDetails, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Redis 中保存登录态对象的键前缀。
     */
    public static final String REDIS_KEY_PREFIX = "login:user:";
    /**
     * Spring Security authority 角色前缀，与安全配置中的 ROLE_* 保持一致。
     */
    private static final String ROLE_PREFIX = "ROLE_";
    /**
     * 系统中表示账号启用状态的业务枚举值。
     */
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

    /**
     * 生成用户登录态在 Redis 中的缓存键。
     *
     * @param userId 用户主键
     * @return Redis 登录态键名
     */
    public static String redisKey(Long userId) {
        return REDIS_KEY_PREFIX + userId;
    }

    /**
     * 生成当前用户登录态在 Redis 中的缓存键。
     *
     * @return Redis 登录态键名
     */
    public String redisKey() {
        return redisKey(id);
    }

    /**
     * 将登录用户转换为轻量认证主体，供 JWT 和请求上下文使用。
     *
     * @return 包含用户和角色信息的认证主体
     */
    public AdminAuthPrincipal toPrincipal() {
        return new AdminAuthPrincipal(id, userName, roleId, roleCode);
    }

    /**
     * 返回当前用户在 Spring Security 中参与授权判断的权限集合。
     *
     * @return ROLE_* 格式的权限集合
     */
    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return permissions == null
                ? List.of()
                : permissions.stream().map(SimpleGrantedAuthority::new).toList();
    }

    /**
     * 返回 Spring Security 认证所需的密码散列。
     *
     * @return 数据库中的 password_hash
     */
    @Override
    @JsonIgnore
    public String getPassword() {
        return passwordHash;
    }

    /**
     * 返回 Spring Security 认证所需的用户名。
     *
     * @return 系统登录用户名
     */
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

    /**
     * 判断账号是否启用。
     *
     * @return userStatus 等于 ENABLED 时返回 true
     */
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
