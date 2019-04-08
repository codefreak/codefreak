# -*- mode: ruby -*-
# vi: set ft=ruby :

Vagrant.configure("2") do |config|
  # For a complete reference, please see the online documentation at
  # https://docs.vagrantup.com.

  # Prefer bento box over official ubuntu ones
  config.vm.box = "bento/ubuntu-18.04"

  # Create a private network and give VM a static IP
  config.vm.network "private_network", ip: "10.12.12.100"

  config.vm.network "forwarded_port", guest: 2375, host: 2375

  # Enable the automatic install of docker and make it available via TCP
  # We bind to 0.0.0.0 because the VM and Host are on a private network
  # The Docker daemon is then available at 10.12.12.100:2375
  # Access via command-line e.g.:
  #
  # $ export DOCKER_HOST='tcp://10.12.12.100:23750'
  # $ docker ps -a
  #
  config.vm.provision "docker" do |d|
    # Make daemon accessible via tcp and restart to apply changes
    d.post_install_provision "shell", inline: <<-eol
      sed -i '/ExecStart=/c\ExecStart=/usr/bin/dockerd -H fd:// -H tcp://0.0.0.0:2375 --containerd=/run/containerd/containerd.sock' /lib/systemd/system/docker.service \
      && systemctl daemon-reload \
      && systemctl restart docker.service
      eol
  end
end
