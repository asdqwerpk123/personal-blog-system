package org.example.personalblogsystem.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.example.personalblogsystem.entity.BlogFriendLink;

public interface IBlogFriendLinkService extends IService<BlogFriendLink> {

    Page<BlogFriendLink> pageFriendLinks(long current, long size, String keyword, String status);

    BlogFriendLink createFriendLink(BlogFriendLink friendLink);

    BlogFriendLink updateFriendLink(Long id, BlogFriendLink friendLink);

    BlogFriendLink updateFriendLinkStatus(Long id, String status);

    boolean deleteFriendLink(Long id);
}
