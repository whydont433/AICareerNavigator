package navigator.trajectory.api;

import navigator.common.Grade;

/** Целевая роль пользователя. Задаётся командой role select. **/
public record UserTarget(String roleId, Grade grade) {
}