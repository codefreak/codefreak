package de.code_freak.codefreak.repository

import de.code_freak.codefreak.entity.FileCollection
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
@ConditionalOnProperty(name = ["code-freak.files.adapter"], havingValue = "JPA")
interface FileCollectionRepository : CrudRepository<FileCollection, UUID>
