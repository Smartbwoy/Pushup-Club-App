package com.example.data.model

data class TrainingProgram(
    val id: String,
    val name: String,
    val description: String,
    val difficulty: String, // "Beginner", "Intermediate", "Advanced"
    val sets: List<Int>, // Reps per set
    val restTimeSeconds: Int
) {
    companion object {
        val programs = listOf(
            TrainingProgram(
                id = "beginner",
                name = "Beginner Foundations",
                description = "Build solid pushing strength with low reps and relaxed, comfortable rest times.",
                difficulty = "Beginner",
                sets = listOf(5, 5, 4, 4, 3),
                restTimeSeconds = 45
            ),
            TrainingProgram(
                id = "fat_loss",
                name = "High-Intensity Endurance",
                description = "Short rest intervals with high volume to boost heart rate and build core muscle stamina.",
                difficulty = "Intermediate",
                sets = listOf(15, 12, 10, 8, 12),
                restTimeSeconds = 20
            ),
            TrainingProgram(
                id = "military_prep",
                name = "Military Prep Drills",
                description = "Simulate official military physical standards with high volume, structured repetitions.",
                difficulty = "Advanced",
                sets = listOf(25, 20, 15, 15, 10),
                restTimeSeconds = 30
            ),
            TrainingProgram(
                id = "challenge_100",
                name = "100 Push-up Pyramids",
                description = "The ultimate progressive overload template configured to unlock 100 reps capacity.",
                difficulty = "Advanced",
                sets = listOf(30, 25, 20, 15, 10),
                restTimeSeconds = 60
            )
        )
    }
}
