# add-public-moments-square

## Why

P2 动态发现需要一个基础公开广场，让用户可以浏览非好友发布的公开动态。当前动态流只覆盖本人和互关好友，无法承载 QQ-like 的发现入口。

## What Changes

- 发布动态时支持 `visibility`：0-好友可见，1-公开。
- 新增 `GET /chat/moment/public/list` 返回公开、正常、审核通过的动态。
- 公开动态允许非好友查看与轻量互动，好友可见动态仍保持原权限边界。

## Non-Goals

- 不做复杂推荐、标签、地理位置、二级评论或多端页面。
- 不引入新的动态表或发现服务。

## Linked Issues

- #42
- #43
