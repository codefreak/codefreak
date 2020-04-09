package org.codefreak.codefreak.repository

import org.codefreak.codefreak.entity.FileCollection
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
@ConditionalOnProperty(name = ["codefreak.files.adapter"], havingValue = "JPA")
interface FileCollectionRepository : CrudRepository<FileCollection, UUID>
