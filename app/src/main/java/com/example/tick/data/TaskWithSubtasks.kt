package com.example.tick.data

import androidx.room.Embedded
import androidx.room.Relation

data class TaskWithSubtasks(
    @Embedded val task: Task,
    @Relation(
        parentColumn = "id",
        entityColumn = "parentTaskId"
    )
    val subtasks: List<Subtask>
)