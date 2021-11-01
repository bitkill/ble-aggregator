package org.ruifernandes.ble.aggregator.containers

import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

class MosquittoContainer(imageName: DockerImageName) : GenericContainer<MosquittoContainer>(imageName)