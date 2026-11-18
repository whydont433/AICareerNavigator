package navigator.trajectory.alternative;

//Детали навыка, показанные в альтернативных маршрутах
public record CareerSkill(long skillId, String name, int actualLevel, int requiredLevel,
                          double weight, double priority, boolean blocking) {}

