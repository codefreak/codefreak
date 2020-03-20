# -*- mode: ruby -*-
# vi: set ft=ruby :

Vagrant.configure("2") do |config|
  # For a complete reference, please see the online documentation at
  # https://docs.vagrantup.com.

  # Prefer bento box over official ubuntu ones
  config.vm.box = "bento/ubuntu-18.04"

  # Create a private network and give VM a static IP
  config.vm.network "private_network", ip: "10.12.12.100"

  config.vm.network "forwarded_port", guest: 2375, host: 2375 # Docker daemon
  config.vm.network "forwarded_port", guest: 80, host: 8081 # IDE containers (traefik)
  config.vm.network "forwarded_port", guest: 389, host: 389 # LDAP server

  # Enable the automatic install of docker and make it available via TCP
  # We bind to 0.0.0.0 because the VM and Host are on a private network
  # The Docker daemon is then available at 10.12.12.100:2375
  # Access via command-line e.g.:
  #
  # $ export DOCKER_HOST='tcp://10.12.12.100:23750'
  # $ docker ps -a
  #
  config.vm.provision "docker" do |d|
    # Run Traefik as reverse proxy inside the VM
    # It is available on port 8081 on the host
    d.run "traefik",
      cmd: "--loglevel=info --docker=true --docker.exposedbydefault=false",
      args: "-p 80:80 -v /var/run/docker.sock:/var/run/docker.sock"
    d.run "portainer/portainer", # credentials admin:admin
      cmd: "-H unix:///var/run/docker.sock --admin-password='$2y$05$n8b3wSfBtMdMY1ei4FBx..qbvqlHx7Rpln7Wd61HQYcIJ7pWgGH7q'",
      args: '-v /var/run/docker.sock:/var/run/docker.sock -l="traefik.enable=true" -l="traefik.frontend.rule=PathPrefixStrip: /portainer/" -l="traefik.port=9000" --name portainer'
    # Make daemon accessible via tcp and restart to apply changes
    d.post_install_provision "shell", inline: <<-eol
      sed -i '/ExecStart=/c\ExecStart=/usr/bin/dockerd -H fd:// -H tcp://0.0.0.0:2375 --containerd=/run/containerd/containerd.sock' /lib/systemd/system/docker.service \
      && systemctl daemon-reload \
      && systemctl restart docker.service
      eol
  end

  # install Java 8
  config.vm.provision "shell", inline: <<-SHELL
    add-apt-repository ppa:openjdk-r/ppa -y
    apt-get update
    apt-get -y install openjdk-8-jdk
    update-alternatives --config java
  SHELL

  config.vm.provider "virtualbox" do |vb|
   vb.memory = "4096"
  end
end
