document.addEventListener("DOMContentLoaded", () => {
  const welcomeScreen = document.getElementById("welcome-screen");
  const startBtn = document.getElementById("start-btn");

  startBtn.addEventListener("click", () => {
    welcomeScreen.classList.add("hidden");
    document.querySelector("header").style.display = "block";
    document.getElementById("add-task").style.display = "flex";
    document.getElementById("calendar").style.display = "block";
    generateCalendar();
  });

  const calendarGrid = document.getElementById("calendar-grid");
  const currentMonthLabel = document.getElementById("current-month");
  const taskForm = document.getElementById("task-form");
  const taskInput = document.getElementById("task-input");
  const taskDate = document.getElementById("task-date");
  const taskTime = document.getElementById("task-time");
  const taskNotes = document.getElementById("task-notes");
  const taskTag = document.getElementById("task-tag");
  const editModal = document.getElementById("edit-modal");
  const editForm = document.getElementById("edit-form");
  const editText = document.getElementById("edit-text");
  const editDate = document.getElementById("edit-date");
  const editTime = document.getElementById("edit-time");
  const editNotes = document.getElementById("edit-notes");
  const editTag = document.getElementById("edit-tag");
  const cancelEditBtn = document.getElementById("cancel-edit");

  const confirmDeleteModal = document.getElementById("confirm-delete-modal");
  const confirmDeleteBtn = document.getElementById("confirm-delete");
  const cancelDeleteBtn = document.getElementById("cancel-delete");

  let currentDate = new Date();
  let tasks = [];
  let taskToEditIndex = null;
  let taskToDeleteIndex = null;

  try {
    if (localStorage.getItem('tasks')) {
      tasks = JSON.parse(localStorage.getItem('tasks')) || [];
    }
  } catch (e) {
    alert("⚠️ Error al acceder al almacenamiento local.");
  }

  document.getElementById("prev-month").addEventListener("click", () => {
    currentDate.setMonth(currentDate.getMonth() - 1);
    generateCalendar();
  });

  document.getElementById("next-month").addEventListener("click", () => {
    currentDate.setMonth(currentDate.getMonth() + 1);
    generateCalendar();
  });

  function generateCalendar() {
    calendarGrid.innerHTML = "";
    const year = currentDate.getFullYear();
    const month = currentDate.getMonth();
    const firstDay = new Date(year, month, 1).getDay();
    const totalDays = new Date(year, month + 1, 0).getDate();

    currentMonthLabel.textContent = currentDate.toLocaleDateString('es-ES', {
      month: 'long',
      year: 'numeric'
    });

    for (let i = 0; i < firstDay; i++) {
      const emptyCell = document.createElement("div");
      emptyCell.classList.add("day", "empty");
      calendarGrid.appendChild(emptyCell);
    }

    for (let day = 1; day <= totalDays; day++) {
      const date = new Date(year, month, day);
      const dateString = date.toISOString().split("T")[0];
      const weekday = date.toLocaleDateString('es-ES', { weekday: 'short' });

      const dayCell = document.createElement("div");
      dayCell.classList.add("day");
      dayCell.dataset.date = dateString;

      dayCell.innerHTML = `
        <div class="day-header">${weekday.charAt(0).toUpperCase() + weekday.slice(1)} ${day}</div>
        <div class="tasks"></div>
      `;
      calendarGrid.appendChild(dayCell);
    }

    renderTasks();
  }

  function renderTasks() {
    document.querySelectorAll(".day .tasks").forEach(container => container.innerHTML = "");

    tasks.forEach((task, index) => {
      const container = document.querySelector(`.day[data-date="${task.date}"] .tasks`);
      if (container) {
        const taskDiv = document.createElement("div");
        taskDiv.classList.add("task", task.tag);
        if (task.completed) taskDiv.classList.add("completed");
        taskDiv.dataset.index = index;

        taskDiv.innerHTML = `
          <div class="task-main">
            <span class="task-title">${task.text}${task.time ? ` ${task.time}` :""}</span>
            <div class="task-menu-toggle">⋮</div>
          </div>
          <div class="task-menu hidden">
            <button class="complete-btn">✅ Completar</button>
            <button class="edit-btn">✏️ Editar</button>
            <button class="delete-btn">🗑️ Eliminar</button>
          </div>
        ` 
              if (task.notes) {
  taskDiv.setAttribute("data-notes", task.notes);
};
        container.appendChild(taskDiv);
      }
    });
  }

  function saveTasks() {
    localStorage.setItem('tasks', JSON.stringify(tasks));
  }

  taskForm.addEventListener("submit", e => {
    e.preventDefault();
    const text = taskInput.value.trim();
    const date = taskDate.value;
    const time = taskTime.value;
    const notes = taskNotes.value.trim();
    const tag = taskTag.value;

    if (text && date && tag) {
      tasks.push({ text, date, time, notes, tag, completed: false });
      saveTasks();
      taskInput.value = "";
      taskDate.value = "";
      taskTime.value = "";
      taskNotes.value = "";
      renderTasks();
    }
  });

  calendarGrid.addEventListener("click", e => {
    e.stopPropagation();
    const taskEl = e.target.closest(".task");
    const index = taskEl?.dataset.index;
    if (!taskEl || index === undefined) return;

    if (e.target.classList.contains("complete-btn")) {
      tasks[index].completed = !tasks[index].completed;
      saveTasks();
      renderTasks();
    }

    if (e.target.classList.contains("edit-btn")) {
      openEditModal(index);
    }

    if (e.target.classList.contains("delete-btn")) {
      openDeleteModal(index);
    }

    if (e.target.classList.contains("task-menu-toggle")) {
      const menu = taskEl.querySelector(".task-menu");
      document.querySelectorAll(".task-menu").forEach(m => m.classList.add("hidden"));
      menu.classList.toggle("hidden");
    }
  });

  function openEditModal(index) {
    const task = tasks[index];
    taskToEditIndex = index;
    editText.value = task.text;
    editDate.value = task.date;
    editTime.value = task.time || "";
    editNotes.value = task.notes || "";
    editTag.value = task.tag;
    editModal.classList.remove("hidden");
  }

  editForm.addEventListener("submit", e => {
    e.preventDefault();
    if (taskToEditIndex !== null) {
      tasks[taskToEditIndex].text = editText.value.trim();
      tasks[taskToEditIndex].date = editDate.value;
      tasks[taskToEditIndex].time = editTime.value;
      tasks[taskToEditIndex].notes = editNotes.value.trim();
      tasks[taskToEditIndex].tag = editTag.value;
      saveTasks();
      renderTasks();
      closeEditModal();
    }
  });

  cancelEditBtn.addEventListener("click", closeEditModal);
  function closeEditModal() {
    editModal.classList.add("hidden");
    taskToEditIndex = null;
  }

  function openDeleteModal(index) {
    taskToDeleteIndex = index;
    confirmDeleteModal.classList.remove("hidden");
  }

  confirmDeleteBtn.addEventListener("click", () => {
    if (taskToDeleteIndex !== null) {
      tasks.splice(taskToDeleteIndex, 1);
      saveTasks();
      renderTasks();
      closeDeleteModal();
    }
  });

  cancelDeleteBtn.addEventListener("click", closeDeleteModal);
  function closeDeleteModal() {
    confirmDeleteModal.classList.add("hidden");
    taskToDeleteIndex = null;
  }

  calendarGrid.addEventListener("click", (e) => {
    const dayCell = e.target.closest(".day");
    if (!dayCell || dayCell.classList.contains("empty")) return;

    const date = dayCell.dataset.date;
    const screenIsMobile = window.innerWidth <= 600;

    if (screenIsMobile && date) {
      const tasksForDay = tasks.filter(t => t.date === date);
      const modal = document.getElementById("mobile-day-modal");
      const title = document.getElementById("modal-day-title");
      const list = document.getElementById("modal-task-list");

      const dateObj = new Date(date);
      title.textContent = dateObj.toLocaleDateString('es-ES', {
        weekday: 'long', day: 'numeric', month: 'long'
      });

      list.innerHTML = tasksForDay.length
        ? tasksForDay.map(t => `
          <div class="modal-task ${t.tag} ${t.completed ? 'completed' : ''}">
            <strong>${t.text}</strong>
            ${t.time ? `<small>🕒 ${t.time}</small>` : ""}
            ${t.notes ? `<p>${t.notes}</p>` : ""}
          </div>`).join("")
        : "<p>No hay tareas.</p>";

      modal.classList.remove("hidden");
    }
  });
  
   document.addEventListener("click", (e) => {
    const isMenu = e.target.closest(".task-menu");
    const isToggle = e.target.classList.contains("task-menu-toggle");
    if (!isMenu && !isToggle) {
      document.querySelectorAll(".task-menu").forEach(menu => menu.classList.add("hidden"));
    }
  });


  document.getElementById("close-mobile-modal").addEventListener("click", () => {
    document.getElementById("mobile-day-modal").classList.add("hidden");
  });

  document.querySelector("header").style.display = "none";
  document.getElementById("add-task").style.display = "none";
  document.getElementById("calendar").style.display = "none";
});
