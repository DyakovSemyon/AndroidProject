package com.example.coflow.models

/**
 * Перечисление ролей пользователя в проекте.
 *
 * @property code Числовой код роли, который сохраняется в Firestore для экономии места и оптимизации запросов.
 */

enum class Role(val code: Int) {
    MEMBER(0),
    OWNER(1);

    companion object {
        fun fromInt(value: Int): Role {
            return entries.firstOrNull { it.code == value } ?: MEMBER
        }
    }
}
