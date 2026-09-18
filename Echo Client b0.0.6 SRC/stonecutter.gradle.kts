plugins {
    id("dev.kikugie.stonecutter")
    id("fabric-loom") version "1.16-SNAPSHOT" apply false
}

stonecutter active "1.21.11" /* [SC] DO NOT EDIT */
// Active version for Stonecutter. (I think)

stonecutter parameters {
    // Add version swaps and constants here as needed
    
    // change these constants to switch between dev and release builds, then reset active project
    // let Cyde take care of this, u only need to have debug set to true and then run "reset active project"
    constants["debug"] = false // true
    constants["release"] = false // false
    constants["auth"] = true // false
}
