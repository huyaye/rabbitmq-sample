#!/bin/sh

# 관리자(root) 권한으로 실행하자.

rabbitmqctl delete_user ccm-dev
rabbitmqctl delete_user ccm-admin
rabbitmqctl delete_vhost ccm-dev-vhost

