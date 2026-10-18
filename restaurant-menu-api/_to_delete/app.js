'use strict';

const crypto = require('crypto');
const express = require('express');
const { validateBody, parseListQuery } = require('./validation');

/** Build a fresh app with its own empty in-memory store. */
function createApp() {
  const app = express();
  app.disable('x-powered-by');

  let dishes = []; // starts empty on every server start, kept in creation order

  app.use(express.json());

  // Malformed JSON / oversized body -> JSON 400 / 413 (checked right after the parser)
  app.use((err, req, res, next) => {
    if (err && err.type === 'entity.parse.failed') return res.status(400).json({ error: 'Malformed JSON' });
    if (err && err.type === 'entity.too.large') return res.status(413).json({ error: 'Payload too large' });
    return next(err);
  });

  const find = (id) => dishes.findIndex((d) => d.id === id);

  // A. Create
  app.post('/api/dishes', (req, res) => {
    const r = validateBody(req.body, false);
    if (r.error) return res.status(400).json({ error: r.error });
    const dish = { id: crypto.randomUUID(), ...r.data };
    dishes.push(dish);
    res.status(201).json(dish);
  });

  // C. List / filter / search / sort / paginate
  app.get('/api/dishes', (req, res) => {
    const p = parseListQuery(req.query);
    if (p.error) return res.status(400).json({ error: p.error });
    const q = p.query;

    // 1. filters
    let result = dishes.filter((d) => {
      if (q.category !== undefined && d.category !== q.category) return false;
      if (q.isVegetarian !== undefined && d.isVegetarian !== q.isVegetarian) return false;
      if (q.minPrice !== undefined && d.price < q.minPrice) return false;
      if (q.maxPrice !== undefined && d.price > q.maxPrice) return false;
      return true;
    });

    // 2. search (dishName OR restaurant, case-insensitive partial)
    if (q.search !== undefined) {
      result = result.filter(
        (d) => d.dishName.toLowerCase().includes(q.search) || d.restaurant.toLowerCase().includes(q.search)
      );
    }

    // 3. sort (stable; ties keep creation order)
    if (q.sort) {
      const dir = q.order === 'desc' ? -1 : 1;
      const key =
        q.sort === 'dishName' ? (d) => d.dishName.toLowerCase() : (d) => d[q.sort];
      result = result
        .map((d, i) => ({ d, i }))
        .sort((a, b) => {
          const x = key(a.d);
          const y = key(b.d);
          if (x < y) return -dir;
          if (x > y) return dir;
          return a.i - b.i;
        })
        .map((o) => o.d);
    }

    // 4. paginate
    const total = result.length;
    const totalPages = total === 0 ? 0 : Math.ceil(total / q.limit);
    const start = (q.page - 1) * q.limit;
    res.status(200).json({
      data: result.slice(start, start + q.limit),
      page: q.page,
      limit: q.limit,
      total,
      totalPages,
    });
  });

  // B. Get one
  app.get('/api/dishes/:id', (req, res) => {
    const i = find(req.params.id);
    if (i === -1) return res.status(404).json({ error: 'Dish not found' });
    res.status(200).json(dishes[i]);
  });

  // D. Replace
  app.put('/api/dishes/:id', (req, res) => {
    const i = find(req.params.id);
    if (i === -1) return res.status(404).json({ error: 'Dish not found' });
    const r = validateBody(req.body, false);
    if (r.error) return res.status(400).json({ error: r.error });
    dishes[i] = { id: dishes[i].id, ...r.data };
    res.status(200).json(dishes[i]);
  });

  // E. Partial update
  app.patch('/api/dishes/:id', (req, res) => {
    const i = find(req.params.id);
    if (i === -1) return res.status(404).json({ error: 'Dish not found' });
    const r = validateBody(req.body, true);
    if (r.error) return res.status(400).json({ error: r.error });
    dishes[i] = { ...dishes[i], ...r.data, id: dishes[i].id };
    res.status(200).json(dishes[i]);
  });

  // F. Delete
  app.delete('/api/dishes/:id', (req, res) => {
    const i = find(req.params.id);
    if (i === -1) return res.status(404).json({ error: 'Dish not found' });
    dishes.splice(i, 1);
    res.status(204).end();
  });

  // Unknown routes -> JSON 404
  app.use((req, res) => res.status(404).json({ error: 'Route not found' }));

  // Last-resort handler: never crash, always JSON
  // eslint-disable-next-line no-unused-vars
  app.use((err, req, res, next) => {
    const status = err && Number.isInteger(err.status) && err.status >= 400 && err.status < 500 ? err.status : 500;
    res.status(status).json({ error: status === 500 ? 'Internal server error' : 'Bad request' });
  });

  return app;
}

module.exports = { createApp };
