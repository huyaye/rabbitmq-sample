#!/bin/sh

# 관리자(root) 권한으로 실행하자.

rabbitmqctl add_vhost ccm-dev-vhost

rabbitmqctl add_user ccm-admin hare123
rabbitmqctl set_permissions -p ccm-dev-vhost ccm-admin ".*" ".*" ".*"
rabbitmqctl set_user_tags ccm-admin administrator

rabbitmqctl add_user ccm-dev coney123
rabbitmqctl set_permissions -p ccm-dev-vhost ccm-dev ".*" ".*" ".*"


